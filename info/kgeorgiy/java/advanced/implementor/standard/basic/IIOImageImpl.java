package info.kgeorgiy.java.advanced.implementor.standard.basic;
class IIOImageImpl extends IIOImage {

    public IIOImageImpl(java.awt.image.RenderedImage arg0, java.util.List arg1, javax.imageio.metadata.IIOMetadata arg2)  {
        super(arg0, arg1, arg2);
   }

    public IIOImageImpl(java.awt.image.Raster arg0, java.util.List arg1, javax.imageio.metadata.IIOMetadata arg2)  {
        super(arg0, arg1, arg2);
   }
}